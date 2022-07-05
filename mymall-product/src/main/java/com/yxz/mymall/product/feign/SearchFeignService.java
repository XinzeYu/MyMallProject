package com.yxz.mymall.product.feign;

import com.yxz.common.es.SkuEsModel;
import com.yxz.common.utils.R;
import lombok.experimental.FieldDefaults;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("mymall-search")
public interface SearchFeignService {
    @PostMapping(value = "/search/save/product")
    R productStatusUp(@RequestBody List<SkuEsModel> skuEsModels);
}
