package com.meridian.parcel.application;

import com.meridian.parcel.domain.ReviewItem;

import java.util.List;

public interface ReviewQueue {

    void add(ReviewItem item);

    List<ReviewItem> pending();
}
