package eu.jlavocat.spacetimedb;

import eu.jlavocat.spacetimedb.bsatn.BsatnReader;

@FunctionalInterface
public interface RowDecoder<T> {
    T decode(BsatnReader reader);
}
