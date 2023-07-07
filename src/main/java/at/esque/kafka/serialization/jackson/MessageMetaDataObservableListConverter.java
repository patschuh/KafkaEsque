package at.esque.kafka.serialization.jackson;

import at.esque.kafka.topics.metadata.MessageMetaData;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class MessageMetaDataObservableListConverter implements Converter<List<MessageMetaData>, ObservableList<MessageMetaData>> {


    @Override
    public ObservableList<MessageMetaData> convert(List<MessageMetaData> headers) {
        return FXCollections.observableArrayList(headers);
    }

    @Override
    public JavaType getInputType(TypeFactory typeFactory) {
        return typeFactory.constructCollectionType(List.class, MessageMetaData.class);
    }

    @Override
    public JavaType getOutputType(TypeFactory typeFactory) {
        return typeFactory.constructCollectionType(ObservableList.class, MessageMetaData.class);
    }
}
