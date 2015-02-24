package models;

import com.atsid.play.models.AbstractBaseModel;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Created by davidtittsworth on 2/23/15.
 */
@Entity
public class TestYetAnotherModel extends AbstractBaseModel {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    public Long id;

}
