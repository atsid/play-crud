package models;

import com.atsid.play.models.AbstractBaseModel;
import com.atsid.play.models.CascadeDelete;
import com.atsid.play.models.schema.FieldDescription;
import com.atsid.play.models.schema.FieldType;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * Created by davidtittsworth on 2/20/15.
 */
@Entity
public class TestModel extends AbstractBaseModel {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    public Long id;

    public String myString;

    public Date myDate;

    @FieldDescription(type = FieldType.DATETIME)
    public Date myDateTime;

    @OneToMany(cascade = CascadeType.REMOVE)
    public List<TestNestedModel> nestedModel;
}
