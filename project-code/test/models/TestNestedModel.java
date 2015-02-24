package models;

import com.atsid.play.models.AbstractBaseModel;
import com.atsid.play.models.CascadeDelete;
import com.atsid.play.models.schema.FieldDescription;
import com.atsid.play.models.schema.FieldType;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by davidtittsworth on 2/20/15.
 */
@Entity
public class TestNestedModel extends AbstractBaseModel {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    public Long id;

    public String myString;

    public Date myDate;

    @FieldDescription(type = FieldType.DATETIME)
    public Date myDateTime;

    @ManyToOne
    @JsonIgnore
    public TestModel testModel;
}
