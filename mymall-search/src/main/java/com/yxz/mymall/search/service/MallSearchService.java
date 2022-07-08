package com.yxz.mymall.search.service;

import com.yxz.mymall.search.vo.SearchParam;
import com.yxz.mymall.search.vo.SearchResult;


public interface MallSearchService {

    /**
     * 返回检索结果
     * @param param
     * @return
     */
    SearchResult search(SearchParam param);
}
