package com.ikeu.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ikeu.model.entity.TransactionRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资金流水 Mapper，提供交易记录实体的基础 CRUD 操作。
 * @author ikeu
 * @since 2025/05/21
 */
@Mapper
public interface TransactionRecordMapper extends BaseMapper<TransactionRecord> {
}
