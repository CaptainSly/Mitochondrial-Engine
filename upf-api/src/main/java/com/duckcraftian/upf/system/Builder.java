package com.duckcraftian.upf.system;

/**
 * Abstract base class for type-safe builders using the template pattern (CRTP).
 *
 * <p>Concrete builders should extend this class, binding {@code T} to
 * themselves and {@code R} to the type they produce. This ensures that
 * methods inherited from this class return the concrete builder type rather
 * than the abstract base, allowing fluent method chaining without unchecked
 * casts in subclasses.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class WeaponRecord extends AbstractRecord { ... }
 *
 * public class WeaponBuilder extends Builder<WeaponBuilder, WeaponRecord> {
 *
 *     @Override
 *     public WeaponBuilder self() { return this; }
 *
 *     @Override
 *     public WeaponRecord build() { return new WeaponRecord(...); }
 * }
 * }</pre>
 *
 *
 * @param <T> the concrete builder type (must be the subclass itself)
 * @param <R> the type of object this builder produces
 */
public abstract class Builder<T extends Builder<T, R>, R> {
    /**
     * Returns this builder as its concrete type.
     *
     * <p>Subclasses must implement this as {@code return this;}.
     * It exists solely to give the abstract base class a reference to the
     * concrete subtype, enabling type-safe method chaining.
     *
     * @return this builder instance, typed as {@code T}
     */
    public abstract T self();

    /**
     * Constructs and returns the object this builder is configured to produce.
     *
     * <p>Implementations should validate any required fields and throw an
     * appropriate exception (e.g. {@link IllegalStateException}) if the
     * builder is not in a valid state to produce the result.
     *
     * @return a new instance of {@code R} built from this builder's state
     */
    public abstract R build();

}