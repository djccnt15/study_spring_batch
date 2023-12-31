package com.batch_java.study_spring_batch.settlement_batch.group;

import com.batch_java.study_spring_batch.domain.repository.SettleGroupRepository;
import com.batch_java.study_spring_batch.model.SettleGroupEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SettleGroupItemDbWriter implements ItemWriter<List<SettleGroupEntity>> {
    
    private final SettleGroupRepository settleGroupRepository;
    
    @Override
    public void write(Chunk<? extends List<SettleGroupEntity>> chunk) throws Exception {
        final List<SettleGroupEntity> settleGroups = new ArrayList<>();
        
        chunk.forEach(settleGroups::addAll);
        
        settleGroupRepository.saveAll(settleGroups);
    }
}
