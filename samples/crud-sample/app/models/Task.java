package models;
import com.atsid.play.models.AbstractBaseModel;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by steve on 1/6/14.
 */

@Entity
public class Task extends AbstractBaseModel {

    @Id
    Long id;

    String text;

}
