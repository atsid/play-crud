package models;

import com.atsid.play.models.AbstractBaseModel;
import com.atsid.play.models.CascadeDelete;
import com.atsid.play.models.schema.FieldDescription;
import com.atsid.play.models.schema.FieldType;
import com.avaje.ebean.annotation.CreatedTimestamp;
import play.data.format.Formats;
import play.data.validation.Constraints;

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

    @Constraints.Required
    public String myString;

    public Date myDate;

    @FieldDescription(type = FieldType.DATETIME)
    public Date myDateTime;

    @Formats.DateTime(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    public Date myTimeStamp;

    public String randomFieldCrud;

    @OneToMany(cascade = CascadeType.REMOVE)
    public List<TestNestedModel> nestedModel;
}
