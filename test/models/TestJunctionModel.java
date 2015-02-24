package models;

import com.atsid.play.models.AbstractBaseModel;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

/**
 * Created by davidtittsworth on 2/23/15.
 */
@Entity
public class TestJunctionModel extends AbstractBaseModel {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    @JsonIgnore
    public TestModel testModel;

    @ManyToOne
    @JsonIgnore
    public TestOtherModel testOtherModel;
}
