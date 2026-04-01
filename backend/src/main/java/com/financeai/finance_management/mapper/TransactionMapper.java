package com.financeai.finance_management.mapper;

import com.financeai.finance_management.converter.DateTimeEpochConverter;
import com.financeai.finance_management.dto.request.UpsertTransactionRequest;
import com.financeai.finance_management.dto.response.TransactionResponse;
import com.financeai.finance_management.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperConfig.class, uses = {DateTimeEpochConverter.class})
public interface TransactionMapper {

    @Mapping(target = "user",  ignore = true)
    @Mapping(target = "category",  ignore = true)
    void partialUpdate(@MappingTarget Transaction transaction, UpsertTransactionRequest request);

    @Mapping(target = "transactionDate", source = "createdAt")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryIcon", source = "category.icon")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categoryColor", source = "category.color")
    TransactionResponse toResponse(Transaction transaction);

}
