package it.unipi.model.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.unipi.model.PostingListInterface;

import java.util.ArrayList;

public class PostingList implements PostingListInterface {
    @JsonProperty("docIdList")
    ArrayList<Integer> docIdList;
    @JsonProperty("termFrequencyList")
    ArrayList<Integer> termFrequencyList;

    public PostingList(){
        docIdList = new ArrayList<>();
        termFrequencyList = new ArrayList<>();
    }

    @Override
    public int docId() {
        return 0;
    }

    @Override
    public double score() {
        return 0;
    }

    @Override
    public void next() {

    }

    @Override
    public void nextGEQ(int docId) {

    }

    @Override
    public void addPosting(int value){
        int last_index = docIdList.size()-1;
        if (docIdList.size()==0 || docIdList.get(last_index)!=value){
            docIdList.add(value);
            termFrequencyList.add(1);
        }
        else termFrequencyList.set(last_index, termFrequencyList.get(last_index)+1);
    }

    @Override
    public String toString(){
        return "DocIdList: "+docIdList.toString()+" TermFrequencyList: "+termFrequencyList.toString();
    }
}
