package com.github.tusharnaik.stringextractor;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimedResponse<T> {
    public long time;
    public T response;
}
