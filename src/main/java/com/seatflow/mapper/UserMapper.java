package com.seatflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seatflow.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM t_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    User selectByUsername(String username);
}
