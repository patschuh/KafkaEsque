package at.esque.kafka.serialization.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.kafka.common.header.Header;

import java.util.List;

public class HeaderObservableListConverter implements Converter<List<Header>, ObservableList<Header>> {


    @Override
    public ObservableList<Header> convert(List<Header> headers) {
        return FXCollections.observableArrayList(headers);
    }

    @Override
    public JavaType getInputType(TypeFactory typeFactory) {
        return typeFactory.constructCollectionType(List.class, Header.class);
    }

    @Override
    public JavaType getOutputType(TypeFactory typeFactory) {
        return typeFactory.constructCollectionType(ObservableList.class, Header.class);
    }
}
