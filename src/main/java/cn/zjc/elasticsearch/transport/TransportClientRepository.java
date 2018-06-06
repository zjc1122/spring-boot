package cn.zjc.elasticsearch.transport;

import cn.zjc.elasticsearch.ESearchTypeColumn;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.min.InternalMin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by zhangjiacheng on 2018/03/11.
 */
@Service
public class TransportClientRepository {

    @Autowired
    private TransportClient transportClient;

    private static final Logger logger = LoggerFactory.getLogger(TransportClientRepository.class);

    /**
     * 创建文档
     * 如果索引和类型不存在会同时创建
     * @param index 索引名称
     * @param type 索引类型
     * @param obj
     * 将bean转化成json格式传入即可，bean需要赋值
     * @return
     */
    public String createIndexAndDocument(String index, String type, String obj){
        IndexResponse response = transportClient
                .prepareIndex(index, type)
                .setSource(obj, XContentType.JSON)
                .execute()
                .actionGet();
        return response.getIndex();
    }

    public String createIndexAndDocument(String index, String type, Object obj){
        IndexResponse response = transportClient
                .prepareIndex(index, type)
                .setSource(getXContentBuilderKeyValue(obj))
                .execute()
                .actionGet();
        return response.getIndex();
    }

    /**
     * 创建索引并添加映射(不包含文档数据)
     * 一次性创建index和mapping
     * @param index 索引名称
     * @param type 索引类型
     * @param obj
     */
    public Boolean createIndexAndMapping(String index, String type, Object obj){

        if (isIndexExist(index)) {
            logger.error("{}索引已经存在", index);
            return false;
        }
        CreateIndexRequestBuilder  createIndexRequestBuilder = null;
        CreateIndexResponse createIndexResponse = null;
        try {
            createIndexRequestBuilder = transportClient
                    .admin()
                    .indices()
                    .prepareCreate(index)
                    .setSettings(Settings.builder().put("index.number_of_shards", 5).put("index.number_of_replicas", 1));
            XContentBuilder builder = getXContentBuilderKeyAndType(obj);
            createIndexResponse = createIndexRequestBuilder.addMapping(type, builder).execute().actionGet();
            logger.info("添加索引成功");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return createIndexResponse.isShardsAcked();
    }

    /**
     * 创建索引
     * @param index 索引名称
     */
    public Boolean createIndex(String index) {
        CreateIndexResponse indexResponse;
        Boolean acknowledged = false;
        try {
            if(isIndexExist(index)){
                return false;
            }
            indexResponse = transportClient
                    .admin()
                    .indices()
                    .prepareCreate(index)
                    .setSettings(Settings.builder().put("index.number_of_shards", 5).put("index.number_of_replicas", 1))
                    .execute()
                    .actionGet();
            acknowledged = indexResponse.isAcknowledged();
        } catch (ElasticsearchException e) {
            e.printStackTrace();
        }
        return acknowledged;
    }

    /**
     * 删除索引
     * @param index 索引名称
     */
    public Boolean deleteIndex(String index){
        Boolean acknowledged = false;
        DeleteIndexResponse deleteIndexResponse ;
        if (!isIndexExist(index)) {
            logger.error("{}索引不存在", index);
            return false;
        }
        try {
            deleteIndexResponse = transportClient
                    .admin()
                    .indices()
                    .prepareDelete(index)
                    .execute()
                    .actionGet();

            acknowledged = deleteIndexResponse.isAcknowledged();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return acknowledged;
    }

    /**
     * 判断索引是否存在
     *
     * @param index
     * @return
     */
    public Boolean isIndexExist(String index) {

        return transportClient.admin().indices().prepareExists(index).execute().actionGet().isExists();
    }

    /**
     * 创建type和mapping
     * index必须存在否则添加失败
     * type和mapping存在则进行更新
     * @param index
     * @param type
     * @param obj
     * @return
     */
    public Boolean createTypeAndMapping(String index, String type, Object obj) {
        Boolean acknowledged = false;
        if (!isIndexExist(index)) {
            logger.error("{}索引不存在", index);
            return false;
        }
        try {
            PutMappingResponse putMappingResponse = transportClient
                    .admin()
                    .indices()
                    .preparePutMapping(index)
                    .setType(type)
                    .setSource(getXContentBuilderKeyAndType(obj))
                    .execute()
                    .actionGet();

            acknowledged = putMappingResponse.isAcknowledged();

            return acknowledged;
        } catch (Exception e) {
            logger.error("创建type失败，{}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 判断type是否存在
     * @param index
     * @param type
     * @return
     */
    public Boolean isTypeExist(String index, String type) {
        return transportClient.admin().indices().prepareTypesExists(index).setTypes(type).execute().actionGet().isExists();
    }

    /**
     * 给索引增加mapping
     * index必须存在
     * @param index
     * @param type
     * @param obj
     */
    public Boolean creatMapping(String index, String type, Object obj) {
        Boolean acknowledged = false;
        if (!isIndexExist(index)) {
            logger.error("{}索引不存在", index);
            return false;
        }
        try {
            XContentBuilder builder = getXContentBuilderKeyAndType(obj);
            PutMappingRequest mappingRequest = Requests.putMappingRequest(index).source(builder).type(type);
            PutMappingResponse putMappingResponse = transportClient.admin().indices().putMapping(mappingRequest).actionGet();
            acknowledged = putMappingResponse.isAcknowledged();
        } catch (ElasticsearchException e) {
            e.printStackTrace();
        }
        return acknowledged;
    }

    public void updateDocumentforJson(String index, String type,String id, String obj){

        UpdateResponse updateResponse = transportClient
                .prepareUpdate()
                .setIndex(index)
                .setType(type)
                .setId(id)
                .setDoc(obj, XContentType.JSON)
                .execute()
                .actionGet();
    }
    public void updateDocumentforBuilder(String index, String type,String id, Object obj){

        UpdateResponse updateResponse = transportClient
                .prepareUpdate()
                .setIndex(index)
                .setType(type)
                .setId(id)
                .setDoc(getXContentBuilderKeyValue(obj))
                .execute()
                .actionGet();
    }

    /**
     * 根据ID查询一条数据记录
     * @param index
     * @param type
     * @param id
     * @return
     */
    public String getDocumentById(String index, String type, String id){

        GetResponse getResponse = transportClient
                .prepareGet()
                .setIndex(index)
                .setType(type)
                .setId(id)
                .execute()
                .actionGet();
        return getResponse.getSourceAsString();
    }

    /**
     * 根据ID删除一条数据记录
     * @param index
     * @param type
     * @param id
     * @return
     */
    public Integer deleteDocument(String index, String type, String id) {
        DeleteResponse deleteResponse  = transportClient
                .prepareDelete()
                .setIndex(index)
                .setType(type)
                .setId(id)
                .execute()
                .actionGet();

        return deleteResponse.status().getStatus();
    }

    /**
     * 查询索引或者索引下的type中的文档
     * @param index
     * @param type
     * @return
     * @throws Exception
     */
    public SearchResponse matchAllQuery(String index,String type) throws Exception{
        SearchResponse response;
        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        if(StringUtils.isNotBlank(type)){
            response = transportClient.prepareSearch(index).setTypes(type).setQuery(queryBuilder).execute().actionGet();
        }else {
            response = transportClient.prepareSearch(index).setQuery(queryBuilder).execute().actionGet();
        }

        return response;
    }
    /**
     * 根据条件查询
     * @param index
     * @param type
     * @param field
     * @param value
     * @return
     */
    public List<String> queryByFilter(String index, String type,String field,String value) {

        QueryBuilder  queryBuilder = QueryBuilders.termQuery(field,value);
        SearchResponse searchResponse =
                transportClient
                        .prepareSearch()
                        .setIndices(index)
                        .setTypes(type)
                        .setPostFilter(queryBuilder)
                        .execute()
                        .actionGet();

        List<String> docList = Lists.newArrayList();
        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit hit : searchHits) {
            docList.add(hit.getSourceAsString());
        }
        return docList;
    }
    /**
     * 根据条件删除文档。
     */
    public Long deleteByQuery(String index,String field,String value ) {
        QueryBuilder queryBuilder = QueryBuilders.matchQuery(field, value);
        BulkByScrollResponse response = DeleteByQueryAction.INSTANCE.newRequestBuilder(transportClient)
                        .filter(queryBuilder)
                        .source(index)
                        .get();
        return response.getDeleted();
    }
    /**
     * 使用min聚合查询字段上最小的值。
     * @param index
     * @param field
     */
    public Double min(String index,String field) {
        SearchResponse response = transportClient
                .prepareSearch(index)
                .addAggregation(AggregationBuilders.min("min").field(field))
                .execute()
                .actionGet();
        InternalMin min = response.getAggregations().get("min");
        return min.getValue();
    }

    /**
     * 得到bean中的key和type
     * @param o
     * @return
     */
    private XContentBuilder getXContentBuilderKeyAndType(Object o) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().startObject().startObject("properties");
            List<Field> fieldList = Lists.newArrayList();
            Class tempClass = o.getClass();
            /**
             * 当父类为null的时候说明到达了最上层的父类(Object类).
             */
            while (tempClass != null) {
                fieldList.addAll(Arrays.asList(tempClass.getDeclaredFields()));
                /**
                 * 得到父类,然后赋给自己
                 */
                tempClass = tempClass.getSuperclass();
            }
            for (Field field : fieldList) {
                if(field.isAnnotationPresent(ESearchTypeColumn.class)) {
                    //是否分词默认为不分词
                    String analyze = "keyword";
                    ESearchTypeColumn annotation = field.getAnnotation(ESearchTypeColumn.class);
                    String type = annotation.type();
                    boolean ifAnalyze = annotation.analyze();
                    //对string类型做分词处理
                    if(type.equals("string")){
                        if(ifAnalyze){
                            analyze = "text";
                            builder.startObject(field.getName());
                            builder.field("type",analyze);
                        }else {
                            builder.startObject(field.getName());
                            builder.field("type",analyze);
                        }
                    }else {
                        builder.startObject(field.getName());
                        builder.field("type",type);
                        //对date类型做时间处理
                        if(type.equals("date")){
                            //设置Date的格式
//                            builder.field("format","yyyy-MM-dd HH:mm:ss");
                        }
                    }
                    builder.endObject();
                }
            }
            builder.endObject();
            builder.endObject();
            logger.info(builder.string());
            return builder;
        } catch (Exception e) {
            logger.error("获取object key-value失败，{}", e.getMessage());
        }
        return null;
    }

    private XContentBuilder getXContentBuilderKeyValue(Object o) {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().startObject();
            List<Field> fieldList = Lists.newArrayList();
            @SuppressWarnings("rawtypes")
            Class tempClass = o.getClass();
            // 当父类为null的时候说明到达了最上层的父类(Object类).
            while (tempClass != null) {
                fieldList.addAll(Arrays.asList(tempClass.getDeclaredFields()));
                // 得到父类,然后赋给自己
                tempClass = tempClass.getSuperclass();
            }
            for (Field field : fieldList) {
                if(field.isAnnotationPresent(ESearchTypeColumn.class)) {
                    PropertyDescriptor descriptor = new PropertyDescriptor(field.getName(), o.getClass());
                    Object value =descriptor.getReadMethod().invoke(o);
                    if (value != null) {
                        builder.field(field.getName(),value.toString());
                    }
                }
            }
            builder.endObject();
            logger.debug(builder.string());
            return builder;
        } catch (Exception e) {
            logger.error("获取object key-value失败，{}", e.getMessage());
        }
        return null;
    }
}
