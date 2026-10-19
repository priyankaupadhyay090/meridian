package com.meridian.change.application;

import com.meridian.change.domain.RuleChange;

import java.util.List;
import java.util.Optional;

public interface RuleChangeRepository {

    void save(RuleChange change);

    Optional<RuleChange> findById(String id);

    List<RuleChange> findAll();
}
