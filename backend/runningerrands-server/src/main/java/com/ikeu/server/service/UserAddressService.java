package com.ikeu.server.service;

import com.ikeu.model.dto.AddressSaveDTO;
import com.ikeu.model.entity.UserAddress;
import com.ikeu.model.vo.AddressVO;

import java.util.List;

/**
 * 用户地址簿服务接口，提供地址的增删改查和默认地址设置功能。
 * @author ikeu
 * @since 2026/05/14
 */
public interface UserAddressService {

    /** 新增地址，关联到当前用户。 */
    void saveAddressInfo(Long userId, AddressSaveDTO addressSaveDTO);

    /** 删除指定地址，校验归属当前用户。 */
    void deleteAddress(Long userId, Long addressId);

    /** 修改指定地址信息，校验归属当前用户。 */
    void updateAddress(Long userId, Long addressId, AddressSaveDTO dto);

    /** 获取当前用户的所有地址列表，默认地址排前。 */
    List<AddressVO> listAddresses(Long userId);

    /** 设置默认地址（先清零所有再设置目标）。 */
    void setDefault(Long userId, Long addressId);
}
