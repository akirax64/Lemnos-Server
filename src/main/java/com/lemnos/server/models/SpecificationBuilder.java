package com.lemnos.server.models;

import org.springframework.data.jpa.domain.Specification;

import java.util.Date;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class SpecificationBuilder<T> {
    private Specification<T> specification;

    public SpecificationBuilder() {
        this.specification = Specification.unrestricted();
    }

    /**
     * Add a specification using {@code .and()}.
     *
     * @param specification The specification to be applied
     *
     * @return The specification with or without the filter
     *
     * @see Specification
     **/
    public SpecificationBuilder<T> add(Specification<T> specification) {
        if (specification != null) {
            this.specification = this.specification.and(specification);
        }
        return this;
    }

    /**
     * Add a specification using {@code .and()}, and if the value passed is not null.
     *
     * @param value The value to be verified as not null
     * @param specificationFunction The specification method who does the search to be applied
     *
     * @return The specification with or without the filter
     *
     * @see Specification
     * @see Function
     **/
    public <V> SpecificationBuilder<T> addIfNotNull(V value, Function<V, Specification<T>> specificationFunction) {
        if (value != null) {
            this.specification = this.specification.and(specificationFunction.apply(value));
        }
        return this;
    }

    /**
     * Add a specification using {@code .and()}, if the value passed is not null and if pass the {@code condition}.
     *
     * @param condition The condition to be used in the {@code value}, then the {@code specificationFunction} is applied
     * @param value The value to be verified as not null
     * @param specificationFunction The specification method who does the search to be applied
     *
     * @return The specification with or without the filter
     *
     * @see Specification
     * @see Function
     * @see Predicate
     **/
    public <V> SpecificationBuilder<T> addIf(Predicate<V> condition, V value, Function<V, Specification<T>> specificationFunction) {
        if (value != null && condition.test(value)) {
            this.specification = this.specification.and(specificationFunction.apply(value));
        }
        return this;
    }

    /**
     * Add a specification using {@code .and()}, using a between search
     * if the two dates are passed
     * and just one if only the start one was passed.
     *
     * @param start The initial date
     * @param end The final date
     * @param rangeSpecification The specification method who does the search between the two dates to be applied
     * @param singleSpecification The specification method who does the search if just the initial date exists to be applied
     *
     * @return The specification with or without the filter
     *
     * @see Specification
     * @see Function
     * @see BiFunction
     **/
    public SpecificationBuilder<T> addDateRange(Date start, Date end, BiFunction<Date, Date, Specification<T>> rangeSpecification, Function<Date, Specification<T>> singleSpecification) {
        if (start != null && end != null) {
            this.specification = this.specification.and(rangeSpecification.apply(start, end));
        }
        else if (start != null) {
            this.specification = this.specification.and(singleSpecification.apply(start));
        }
        return this;
    }

    /**
     * Add a specification using {@code .and()}, using a between search
     * if the two values are passed
     * and just one if only the start one was passed.
     *
     * @param first The initial value
     * @param second The final value
     * @param rangeSpecification The specification method who does the search between the two dates to be applied
     *
     * @return The specification with or without the filter
     *
     * @see Specification
     * @see Function
     * @see BiFunction
     **/
    public SpecificationBuilder<T> addValueRange(Double first, Double second, BiFunction<Double, Double, Specification<T>> rangeSpecification, Double defaultFirstValue) {
        if ((first != null && first >= 0) && (second != null && second >= 0)) {
            this.specification = this.specification.and(rangeSpecification.apply(first, second));
        }
        else if (first == null && (second != null && second >= 0)) {
            this.specification = this.specification.and(rangeSpecification.apply(defaultFirstValue, second));
        }
        return this;
    }

    /**
     * Returns the specification assembled.
     *
     * @return The specification with or without the filter
     **/
    public Specification<T> build() {
        return this.specification;
    }
}