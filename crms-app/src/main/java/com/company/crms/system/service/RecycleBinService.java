package com.company.crms.system.service;

import com.company.crms.common.response.PageResult;
import com.company.crms.system.dto.RecycleBinQuery;
import com.company.crms.system.vo.RecycleBinItemVO;

public interface RecycleBinService {
    PageResult<RecycleBinItemVO> page(RecycleBinQuery query);

    void restore(String bizType, Long id);

    void hardDelete(String bizType, Long id, String reason);
}
