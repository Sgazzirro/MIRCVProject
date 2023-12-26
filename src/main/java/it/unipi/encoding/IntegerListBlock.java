package it.unipi.encoding;

/**
 * Class that describe a block in a compressed integer list
 * @param length length in bytes of the block
 * @param upperbound maximum number in this block
 */
public record IntegerListBlock(int length, int upperbound) { }
