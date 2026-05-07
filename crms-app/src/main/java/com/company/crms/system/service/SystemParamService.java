package com.company.crms.system.service;

import java.util.List;
import com.company.crms.system.entity.SystemParam;

public interface SystemParamService {
    List<SystemParam> listAll();

    String get(String key);

    int getInt(String key, int defaultValue);

    void update(String key, String value);
}
