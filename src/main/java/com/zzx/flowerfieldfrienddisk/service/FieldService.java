package com.zzx.flowerfieldfrienddisk.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.zzx.flowerfieldfrienddisk.common.BaseResponse;
import com.zzx.flowerfieldfrienddisk.model.dto.field.FieldAddRequest;
import com.zzx.flowerfieldfrienddisk.model.dto.field.FieldJoinRequest;
import com.zzx.flowerfieldfrienddisk.model.dto.field.FieldQueryRequest;
import com.zzx.flowerfieldfrienddisk.model.dto.field.FieldQuitRequest;
import com.zzx.flowerfieldfrienddisk.model.entity.Field;
import com.zzx.flowerfieldfrienddisk.model.entity.User;
import com.zzx.flowerfieldfrienddisk.model.vo.FieldUserVO;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
* @author zzx
* @description 针对表【field(花田)】的数据库操作Service
* @createDate 2025-10-17 18:48:45
*/
public interface FieldService extends IService<Field> {

    long addField(FieldAddRequest fieldAddRequest, User loginUser);

    void validField(Field field, boolean add);

    boolean deleteField(long id, User loginUser);


    List<FieldUserVO>  listFields(FieldQueryRequest fieldQueryRequest, boolean isAdmin );

    boolean updateField(Field Field, User loginUser);

    boolean joinField(FieldJoinRequest FieldJoinRequest, User loginUser);

    boolean joinPrivateField(FieldJoinRequest FieldJoinRequest, User loginUser);

    boolean quitField(FieldQuitRequest FieldQuitRequest, User loginUser);

    BaseResponse<List<User>> getFieldMemberListById(Long id ,@RequestParam(required = false) String searchText);
}
