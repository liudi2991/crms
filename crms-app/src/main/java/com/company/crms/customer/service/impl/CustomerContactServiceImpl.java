package com.company.crms.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.crms.common.annotation.OperationLog;
import com.company.crms.common.annotation.SensitiveField;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.security.UserContextHolder;
import com.company.crms.common.util.AesCryptoUtil;
import com.company.crms.common.util.MaskUtil;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.customer.dto.CreateContactDTO;
import com.company.crms.customer.dto.UpdateContactDTO;
import com.company.crms.customer.entity.CustomerContact;
import com.company.crms.customer.mapper.CustomerContactMapper;
import com.company.crms.customer.mapper.CustomerMapper;
import com.company.crms.customer.service.CustomerContactService;
import com.company.crms.customer.vo.CustomerContactVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerContactServiceImpl implements CustomerContactService {

    private final CustomerContactMapper mapper;
    private final CustomerMapper customerMapper;
    private final AesCryptoUtil aes;

    @Override
    public List<CustomerContactVO> listByCustomer(Long customerId) {
        QueryWrapper<CustomerContact> w = new QueryWrapper<CustomerContact>()
                .eq("customer_id", customerId)
                .orderByDesc("is_primary")
                .orderByAsc("id");
        return mapper.selectList(w).stream().map(this::toVO).toList();
    }

    @Override
    @OperationLog(module = "客户", action = "新建联系人", type = "CREATE")
    @Transactional
    public Long create(CreateContactDTO dto) {
        if (customerMapper.selectById(dto.getCustomerId()) == null) {
            throw new BizException(ErrorCode.CU_NOT_FOUND);
        }
        CustomerContact c = new CustomerContact();
        c.setId(SnowflakeIdGenerator.next());
        BeanUtils.copyProperties(dto, c, "phone", "email", "isPrimary");
        c.setPhone(aes.encrypt(dto.getPhone()));
        c.setEmail(aes.encrypt(dto.getEmail()));
        c.setIsPrimary(Boolean.TRUE.equals(dto.getIsPrimary()) ? 1 : 0);
        if (c.getIsPrimary() == 1) {
            mapper.clearPrimary(dto.getCustomerId());
        }
        mapper.insert(c);
        return c.getId();
    }

    @Override
    @OperationLog(module = "客户", action = "更新联系人", type = "UPDATE")
    @Transactional
    public void update(Long id, UpdateContactDTO dto) {
        CustomerContact existing = mapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.CU_NOT_FOUND, "联系人不存在");
        }
        CustomerContact upd = new CustomerContact();
        upd.setId(id);
        upd.setVersion(dto.getVersion());
        upd.setName(dto.getName());
        upd.setTitle(dto.getTitle());
        upd.setPhone(dto.getPhone() == null ? null : aes.encrypt(dto.getPhone()));
        upd.setEmail(dto.getEmail() == null ? null : aes.encrypt(dto.getEmail()));
        upd.setWechat(dto.getWechat());
        upd.setRemark(dto.getRemark());
        int rows = mapper.updateById(upd);
        if (rows == 0) {
            throw new BizException(ErrorCode.CU_NOT_FOUND, "已被其他用户修改，请刷新后重试");
        }
    }

    @Override
    @OperationLog(module = "客户", action = "删除联系人", type = "DELETE")
    @Transactional
    public void remove(Long id) {
        if (mapper.selectById(id) == null) {
            throw new BizException(ErrorCode.CU_NOT_FOUND, "联系人不存在");
        }
        mapper.deleteById(id);
    }

    @Override
    @OperationLog(module = "客户", action = "设为主联系人")
    @Transactional
    public void setPrimary(Long id) {
        CustomerContact c = mapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.CU_NOT_FOUND, "联系人不存在");
        }
        mapper.clearPrimary(c.getCustomerId());
        CustomerContact upd = new CustomerContact();
        upd.setId(id);
        upd.setIsPrimary(1);
        upd.setVersion(c.getVersion());
        mapper.updateById(upd);
    }

    private CustomerContactVO toVO(CustomerContact c) {
        CustomerContactVO vo = new CustomerContactVO();
        BeanUtils.copyProperties(c, vo, "phone", "email", "isPrimary");
        boolean canSeeRaw = UserContextHolder.require().isSuperAdmin();
        String phone = c.getPhone() == null ? null : safeDecrypt(c.getPhone());
        String email = c.getEmail() == null ? null : safeDecrypt(c.getEmail());
        vo.setPhone(canSeeRaw ? phone : MaskUtil.mask(phone, SensitiveField.Mask.PHONE));
        vo.setEmail(canSeeRaw ? email : MaskUtil.mask(email, SensitiveField.Mask.EMAIL));
        vo.setIsPrimary(c.getIsPrimary() != null && c.getIsPrimary() == 1);
        return vo;
    }

    private String safeDecrypt(String enc) {
        try {
            return aes.decrypt(enc);
        } catch (Exception ex) {
            return null;
        }
    }
}
