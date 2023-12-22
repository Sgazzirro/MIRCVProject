package it.unipi.encoding;

import it.unipi.encoding.implementation.EliasFano;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

public class EliasFanoTest {

    private final Encoder eliasFano = new EliasFano();

    @Test
    public void testEncodeDecode() {
        List<Integer> list = List.of(1,2);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }

    @Test
    public void testEncodeDecode01() {
        List<Integer> list = List.of(0, 1);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }

    @Test
    public void testEncodeDecode0(){
        List<Integer> list = List.of(0);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }

    @Test
    public void testEncodeDecode1(){
        List<Integer> list = List.of(1);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }

    @Test
    public void testEncodeDecode2(){
        List<Integer> list = List.of(2);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }

    @Test
    public void testEncodeDecode3(){
        List<Integer> list = List.of(3);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }

    @Test
    public void testEncodeDecode4(){
        List<Integer> list = List.of(4);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }

    @Test
    public void testEncodeDecode5(){
        List<Integer> list = List.of(5);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }

    @Test
    public void testEncodeDecode2_to_30(){
        List<Integer> list = List.of((int) Math.pow(2,30));
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }

    @Test
    public void finalTest() {
        Random random = new Random();
        int numTimes = 10000;
        List<Integer> list = new ArrayList<>();
        int lowerBound = 1;
        int upperBound = 8800000;
        for(int i=0; i<numTimes; i++){
            int numDocIds = random.nextInt(100) + 1;
            for(int j=0; j<numDocIds; j++){
                int randomValue = lowerBound + random.nextInt(upperBound);
                list.add(randomValue);
            }
            Collections.sort(list);
            byte[] bytes = eliasFano.encode(list);
            assertEquals(list, eliasFano.decode(bytes));
            list.clear();
        }
    }
}
