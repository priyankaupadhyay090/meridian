package com.meridian.change.adapter.persistence;

import com.meridian.change.application.RuleChangeRepository;
import com.meridian.change.domain.RuleChange;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRuleChangeRepository implements RuleChangeRepository {

    private final Map<String, RuleChange> changes = new ConcurrentHashMap<>();

    @Override
    public void save(RuleChange change) {
        changes.put(change.id(), change);
    }

    @Override
    public Optional<RuleChange> findById(String id) {
        return Optional.ofNullable(changes.get(id));
    }

    @Override
    public List<RuleChange> findAll() {
        return List.copyOf(changes.values());
    }
}
