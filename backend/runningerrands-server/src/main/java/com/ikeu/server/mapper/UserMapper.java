package com.ikeu.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ikeu.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper，提供用户实体的 CRUD 及行级锁查询。
 * @author ikeu
 * @since 2025/05/21
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 使用 SELECT ... FOR UPDATE 行级锁查询用户，防止余额并发丢失更新。
     *
     * @param id 用户ID
     * @return 用户实体，不存在返回 null
     */
    @Select("SELECT * FROM user WHERE id = #{id} FOR UPDATE")
    User selectByIdForUpdate(@Param("id") Long id);
}
