package com.jointsky.edps.spider.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * edps-spider
 * edps-spider
 * Created by hezl on 2018-10-17.
 */
public class PageConfig implements Serializable {
    /**
     * 页面配置ID
     */
    private String id;
    /**
     * URL 类型
     */
    /**
     * 结果是否为 JSON
     */
    private boolean jsonType;
    /**
     * 是否为结果页
     */
    private boolean targetUrl;
    /**
     * 结果页选择器
     */
    private List<UrlSelectConfig> targetSelect;
    /**
     * 中间页选择器
     */
    private List<HelpSelectConfig> helpSelect;
    /**
     * 字段选择器
     */
    private List<ResultSelectConfig> resultFields;
    /**
     * 静态字段
     */
    private List<FieldSelectConfig> staticFields;
    /**
     * 新 HelpUrl 的配置
     */
    private PageConfig nextHelpConfig;
    /**
     * 新 TargetUrl 的配置
     */
    private PageConfig nextTargetConfig;
    /**
     * 已处理的静态字段
     */
    private List<FieldSelectConfig> dealStaticFields;
    /**
     * 是否全部为静态字段
     */
    private boolean allStaticFiles = true;

    public PageConfig(String id) {
        this.id = id;
        this.jsonType = false;
        this.targetUrl = false;
        this.targetSelect = new ArrayList<>();
        this.helpSelect = new ArrayList<>();
        this.resultFields = new ArrayList<>();
        this.staticFields = new ArrayList<>();
        this.dealStaticFields = new ArrayList<>();
    }

    public boolean isJsonType() {
        return jsonType;
    }

    public void setJsonType(boolean jsonType) {
        this.jsonType = jsonType;
    }

    public boolean isTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(boolean targetUrl) {
        this.targetUrl = targetUrl;
    }

    public List<UrlSelectConfig> getTargetSelect() {
        return targetSelect;
    }

    public void setTargetSelect(List<UrlSelectConfig> targetSelect) {
        this.targetSelect = targetSelect;
    }

    public List<HelpSelectConfig> getHelpSelect() {
        return helpSelect;
    }

    public void setHelpSelect(List<HelpSelectConfig> helpSelect) {
        this.helpSelect = helpSelect;
    }

    public List<ResultSelectConfig> getResultFields() {
        return resultFields;
    }

    public void setResultFields(List<ResultSelectConfig> resultFields) {
        this.resultFields = resultFields;
    }

    public List<FieldSelectConfig> getStaticFields() {
        return staticFields;
    }

    public void setStaticFields(List<FieldSelectConfig> staticFields) {
        this.staticFields = staticFields;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PageConfig getNextHelpConfig() {
        return nextHelpConfig;
    }

    public void setNextHelpConfig(PageConfig nextHelpConfig) {
        this.nextHelpConfig = nextHelpConfig;
    }

    public PageConfig getNextTargetConfig() {
        return nextTargetConfig;
    }

    public void setNextTargetConfig(PageConfig nextTargetConfig) {
        this.nextTargetConfig = nextTargetConfig;
    }

    public List<FieldSelectConfig> getDealStaticFields() {
        return dealStaticFields;
    }

    public void setDealStaticFields(List<FieldSelectConfig> dealStaticFields) {
        this.dealStaticFields = dealStaticFields;
    }

    public boolean isAllStaticFiles() {
        return allStaticFiles;
    }

    public void setAllStaticFiles(boolean allStaticFiles) {
        this.allStaticFiles = allStaticFiles;
    }
}
