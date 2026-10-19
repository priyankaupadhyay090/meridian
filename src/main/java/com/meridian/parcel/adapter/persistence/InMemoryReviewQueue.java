package com.meridian.parcel.adapter.persistence;

import com.meridian.parcel.application.ReviewQueue;
import com.meridian.parcel.domain.ReviewItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryReviewQueue implements ReviewQueue {

    private final List<ReviewItem> items = new CopyOnWriteArrayList<>();

    @Override
    public void add(ReviewItem item) {
        items.add(item);
    }

    @Override
    public List<ReviewItem> pending() {
        return List.copyOf(items);
    }
}
