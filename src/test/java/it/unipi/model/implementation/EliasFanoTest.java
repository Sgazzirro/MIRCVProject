package it.unipi.model.implementation;

import it.unipi.encoding.Encoder;
import it.unipi.encoding.implementation.EliasFano;
import junit.framework.TestCase;
import org.junit.Test;

import java.util.*;

public class EliasFanoTest extends TestCase {

    private Encoder eliasFano = new EliasFano();

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }
    /*
    public void testEncode1(){
        Integer[] array = {3,4,7,13,14,15,21,25,36,38,54,62};
        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(array));
        byte [] compressedBytes = eliasFano.encode(list);
        // U | n | list
        assertEquals( "00000000000000000000000000111110000000000000000000000000000011000111011100110101000001011100111010111011100101111101100100000110",byteArrayToBinaryString(compressedBytes));
    }

    public void testDecode1(){
        String binaryString = "00000000000000000000000000111110000000000000000000000000000011000111011100110101000001011100111010111011100101111101100100000110";
        byte[] byteArray = binaryStringToByteArray(binaryString);
        assertEquals(Arrays.asList(3,4,7,13,14,15,21,25,36,38,54,62), eliasFano.decode(byteArray));
    }

     */

    public void testEncodeDecode() {
        List<Integer> list = List.of(1,2);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }

    public void testEncodeDecode0(){
        List<Integer> list = List.of(0);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }
    public void testEncodeDecode1(){
        List<Integer> list = List.of(1);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }
    public void testEncodeDecode2(){
        List<Integer> list = List.of(2);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }
    public void testEncodeDecode3(){
        List<Integer> list = List.of(3);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }
    public void testEncodeDecode4(){
        List<Integer> list = List.of(4);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }
    public void testEncodeDecode5(){
        List<Integer> list = List.of(5);
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }

    public void testEncodeDecode2_to_30(){
        List<Integer> list = List.of((int)Math.pow(2,30));
        byte[] bytes = eliasFano.encode(list);
        assertEquals(list, eliasFano.decode(bytes));
    }
    @Test
    public void test_finale(){
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
