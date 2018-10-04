package app.tandv.services.model.binding;

import app.tandv.services.data.entity.BookFormat;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * @author Vic on 9/1/2018
 **/
public class BookFormatAdapter extends XmlAdapter<String, BookFormat> {
    @Override
    public BookFormat unmarshal(String value) {
        return BookFormat.valueOf(value);
    }

    @Override
    public String marshal(BookFormat format) {
        return format.name();
    }
}
