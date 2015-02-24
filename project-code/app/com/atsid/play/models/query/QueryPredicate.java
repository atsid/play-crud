package com.atsid.play.models.query;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import play.data.validation.Constraints;

import javax.persistence.*;

import play.data.validation.Constraints;
import com.atsid.play.models.AbstractBaseModel;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;


@MappedSuperclass
@JsonIgnoreProperties(ignoreUnknown = true)  // Says to ignore extra properties on json objects when deserialized (ie id property)
public class QueryPredicate {

    @Constraints.Required
    public Long position;

    @Constraints.Required
    public String field;

    /**
     * TODO Make it an enum?
     * OR or AND
     */
    public String junction;

    @Enumerated(value = EnumType.STRING)
    public Operator operator;

    public String value;
    public Long relatedId;

    public QueryPredicate(String field, String value) {
        this.field = field;
        this.value = value;
        this.operator = Operator.EQUALS;
        this.junction = "AND";
        this.position = 0l;
    }

    public QueryPredicate() {}
}
