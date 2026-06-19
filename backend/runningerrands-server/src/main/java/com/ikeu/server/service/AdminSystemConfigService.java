package com.ikeu.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ikeu.model.dto.SystemConfigBatchUpdateDTO;
import com.ikeu.model.entity.SystemConfig;
import com.ikeu.model.vo.SystemConfigVO;

import java.util.List;

/**
 * 管理端系统配置服务接口
 * @author ikeu
 * @since 2026/06/18
 */
public interface AdminSystemConfigService extends IService<SystemConfig> {

    /**
     * 获取全部分组配置项
     * @return 配置项 VO 列表
     */
    List<SystemConfigVO> listAll();

    /**
     * 批量更新配置值，写入后清除缓存
     * @param dto 批量更新请求 DTO
     */
    void batchUpdate(SystemConfigBatchUpdateDTO dto);
}
