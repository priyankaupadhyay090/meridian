package com.meridian.change.adapter.web;

import jakarta.validation.constraints.Size;

public record ReviewRequest(@Size(max = 500) String comment) {
}
