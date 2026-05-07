package com.company.crms.customer.service;

import com.company.crms.common.response.PageResult;
import com.company.crms.customer.dto.CreateCustomerDTO;
import com.company.crms.customer.dto.CustomerQuery;
import com.company.crms.customer.dto.MergeCustomerDTO;
import com.company.crms.customer.dto.UpdateCustomerDTO;
import com.company.crms.customer.vo.CustomerAggregateVO;
import com.company.crms.customer.vo.CustomerDuplicateVO;
import com.company.crms.customer.vo.CustomerVO;

import java.util.List;

public interface CustomerService {
    PageResult<CustomerVO> page(CustomerQuery query);

    CustomerVO detail(Long id);

    Long create(CreateCustomerDTO dto);

    void update(UpdateCustomerDTO dto);

    void softDelete(Long id);

    void hardDelete(Long id, String reason);

    void disable(Long id);

    void enable(Long id);

    /**
     * 合并：将 {@link MergeCustomerDTO#getMergedIds()} 中的客户全部并入 mainId。
     * 实现需保证：
     * <ul>
     *   <li>所有合同/联系人/附件 customer_id 改写为 mainId；</li>
     *   <li>被合并客户标记 merged_to=mainId, status=MERGED, is_deleted=1；</li>
     *   <li>写 change_log + operation_log。</li>
     * </ul>
     */
    void merge(MergeCustomerDTO dto);

    /** 客户查重：按名称/统一社会信用代码 模糊匹配（排除自己 selfId 与已合并客户）。 */
    List<CustomerDuplicateVO> checkDuplicate(String name, String uscc, Long selfId);

    /** 详情聚合：客户基础信息 + 联系人列表 + 最近合同 + 最近变更日志。 */
    CustomerAggregateVO aggregate(Long id);
}
