package com.jointsky.edps.spider.scheduler;

import cn.hutool.core.codec.Base64Decoder;
import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.log.StaticLog;
import com.alibaba.fastjson.JSON;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.sort.SortOrder;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;

import java.util.*;

/**
 * edps-spider
 * edps-spider
 * Created by hezl on 2018-10-26.
 */
public class EsScheduler extends DbScheduler {

    private static final int FETCH_SIZE = 500;
    private RestHighLevelClient client;

    public EsScheduler(RestHighLevelClient client) {
        this.client = client;
    }

    @Override
    public void resetDuplicateCheck(Task task) {
        String indexName = getIndexName(task);
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(indexName);
        try {
            DeleteIndexResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            if (deleteIndexResponse.isAcknowledged()) {
                StaticLog.info("索引：" + indexName + " 删除成功！");
            } else {
                StaticLog.info("索引：" + indexName + " 删除失败！");
            }
        } catch (ElasticsearchException exception) {
            if (exception.status() == RestStatus.NOT_FOUND) {
                StaticLog.info("索引：" + indexName + " 不存在！");
            }
        } catch (Exception e) {
            StaticLog.error(e);
        }
    }

    private String getIndexName(Task task){
        String URL_PREFIX = "lk_";
        return URL_PREFIX + task.getUUID().toLowerCase();
    }

    @Override
    public boolean isDuplicate(Request request, Task task) {
        GetRequest getRequest = buildGetRequest(getIndexName(task), request);
        try {
            return client.exists(getRequest, RequestOptions.DEFAULT);
        } catch (Exception e) {
            StaticLog.error(e);
        }
        return false;
    }

    public boolean bulkPush(Request[] requests, Task task){
        if (requests.length <= 0){
            return true;
        }
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        String indexName = getIndexName(task);
        for (Request request : requests) {
            String id = SecureUtil.md5(request.getUrl());
            IndexRequest indexRequest = new IndexRequest(indexName, "doc", id);
            Map<String, Object> body = new HashMap<>();
            body.put("req_url", request.getUrl());
            body.put("update_dtm", DateUtil.formatDateTime(new Date()));
            body.put("priority_num", request.getPriority());
            body.put("req_sta", "0");
            body.put("req_bin", Base64Encoder.encode(JSON.toJSONBytes(request)));
            indexRequest.source(body);
            bulkRequest.add(indexRequest);
        }
        try {
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()){
                StaticLog.error(bulkResponse.buildFailureMessage());
                return false;
            }
            else{
                StaticLog.info("链接提交成功：" + bulkRequest.numberOfActions());
                return true;
            }
        } catch (Exception e) {
            StaticLog.error(e);
        }
        return false;
    }

    private GetRequest buildGetRequest(String indexName, Request request) {
        String id = SecureUtil.md5(request.getUrl());
        GetRequest getRequest = new GetRequest(indexName, "doc", id);
        getRequest.fetchSourceContext(new FetchSourceContext(false));
        getRequest.storedFields("_none_");
        return getRequest;
    }

    @Override
    public int getLeftRequestsCount(Task task) {
        String indexName = getIndexName(task);
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.boolQuery().filter(QueryBuilders.termQuery("req_sta", "0")));
        searchSourceBuilder.size(0).fetchSource(false);
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            return (int) searchResponse.getHits().getTotalHits();
        } catch (Exception e) {
            StaticLog.error(e);
        }
        return 0;
    }

    @Override
    public int getTotalRequestsCount(Task task) {
        String indexName = getIndexName(task);
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.size(0).fetchSource(false);
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            return (int) searchResponse.getHits().getTotalHits();
        } catch (Exception e) {
            StaticLog.error(e);
        }
        return 0;
    }

    public List<Request> fetchRequest(Task task) {
        clearConsumedRequest(task);
        List<Request> requestList = new ArrayList<>();
        String indexName = getIndexName(task);
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.boolQuery().filter(QueryBuilders.termQuery("req_sta", "0")));
        searchSourceBuilder.size(FETCH_SIZE).sort("priority_num", SortOrder.DESC).sort("update_dtm", SortOrder.ASC);
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHit[] hits = searchResponse.getHits().getHits();
            if (hits == null || hits.length <= 0) {
                if (isQueueHasValue()){
                    pushWhenNoDuplicate(null, task);
                    return fetchRequest(task);
                }
                return requestList;
            }
            for (SearchHit hit : hits) {
                String reqBin = hit.getSource().get("req_bin").toString();
                requestList.add(JSON.parseObject(Base64Decoder.decodeStr(reqBin), Request.class));
            }
            return requestList;

        } catch (Exception e) {
            StaticLog.error(e);
        }
        return null;
    }

    public void clearConsumedRequest(Task task) {
        if (hasConsumed == null || hasConsumed.size() <= 0){
            return;
        }
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        String indexName = getIndexName(task);
        for (Request request : hasConsumed) {
            String id = SecureUtil.md5(request.getUrl());
            UpdateRequest indexRequest = new UpdateRequest(indexName, "doc", id);
            Map<String, Object> body = new HashMap<>();
            body.put("update_dtm", DateUtil.formatDateTime(new Date()));
            body.put("req_sta", "1");
            indexRequest.doc(body);
            bulkRequest.add(indexRequest);
        }
        try {
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()){
                StaticLog.error(bulkResponse.buildFailureMessage());
            }
            else{
                StaticLog.info("已消费链接提交成功：" + bulkRequest.numberOfActions());
            }
        } catch (Exception e) {
            StaticLog.error(e);
        }
    }
}
