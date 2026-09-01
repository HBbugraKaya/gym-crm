package com.example.gymcrm.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.gymcrm.entity.TrainerWorkload;

@Repository
public interface TrainerWorkloadRepository extends MongoRepository<TrainerWorkload, String> {

}
