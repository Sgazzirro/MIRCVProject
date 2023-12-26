package it.unipi.encoding.implementation;

import java.util.ArrayList;
import java.util.List;

class Utils {

    /**
     * Given a list of integer
     *      n_0, n_1, n_2, ...
     * returns a list containing gaps of two consecutive numbers
     *      n01, n_1-n_0, n_2-n_1, ....
     * @param intList list of integers
     * @return list of gaps
     */
    static List<Integer> computeGaps(List<Integer> intList) {
        List<Integer> gapList = new ArrayList<>(intList.size());

        // Encode the first number and then just the differences
        // between consecutive numbers
        int previous = 0;
        for (int num : intList) {
            gapList.add(num - previous);
            previous = num;
        }

        return gapList;
    }
}
