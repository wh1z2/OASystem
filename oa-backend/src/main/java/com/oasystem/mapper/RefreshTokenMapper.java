package com.oasystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oasystem.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Refresh Token Mapper
 */
@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {

    /**
     * 根据 token 查询有效的 Refresh Token
     */
    @Select("SELECT * FROM refresh_token WHERE token = #{token} AND revoked = 0 LIMIT 1")
    RefreshToken findByToken(@Param("token") String token);

    /**
     * 将指定用户的所有 Refresh Token 标记为已撤销
     */
    @Update("UPDATE refresh_token SET revoked = 1 WHERE user_id = #{userId} AND revoked = 0")
    int revokeByUserId(@Param("userId") Long userId);
}
