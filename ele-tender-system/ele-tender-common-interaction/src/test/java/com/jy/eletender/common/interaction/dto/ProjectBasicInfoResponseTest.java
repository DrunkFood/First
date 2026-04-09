package com.jy.eletender.common.interaction.dto;

import com.jy.eletender.common.interaction.enums.InteractionEvalMethod;
import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProjectBasicInfoResponseTest {

    @Test
    void shouldExposeTenderListPropertyInsteadOfFlatTenderFields() throws Exception {
        Map<String, PropertyDescriptor> descriptors = Arrays.stream(Introspector.getBeanInfo(ProjectBasicInfoResponse.class).getPropertyDescriptors())
                .collect(java.util.stream.Collectors.toMap(PropertyDescriptor::getName, descriptor -> descriptor));

        assertNotNull(descriptors.get("tenderList"));
        assertEquals(List.class, descriptors.get("tenderList").getPropertyType());
        assertNull(descriptors.get("tenderId"));
        assertNull(descriptors.get("tenderCode"));
        assertNull(descriptors.get("tenderName"));
        assertNull(descriptors.get("tenderAmount"));
    }

    @Test
    void shouldExposePurchaseMethodAndEvalMethodProperties() throws Exception {
        Map<String, PropertyDescriptor> descriptors = Arrays.stream(Introspector.getBeanInfo(ProjectBasicInfoResponse.class).getPropertyDescriptors())
                .collect(java.util.stream.Collectors.toMap(PropertyDescriptor::getName, descriptor -> descriptor));

        assertNotNull(descriptors.get("projectNo"));
        assertEquals(String.class, descriptors.get("projectNo").getPropertyType());
        assertNull(descriptors.get("projectCode"));
        assertNotNull(descriptors.get("purchaseMethod"));
        assertEquals(String.class, descriptors.get("purchaseMethod").getPropertyType());
        assertNull(descriptors.get("purchaseContext"));
        assertNotNull(descriptors.get("evalMethod"));
        assertEquals(InteractionEvalMethod.class, descriptors.get("evalMethod").getPropertyType());
        assertNull(descriptors.get("compileScope"));
        assertNull(descriptors.get("tendererId"));
        assertNull(descriptors.get("tendererName"));
    }

    @Test
    void shouldExposeTenderIndexOfProperty() throws Exception {
        Map<String, PropertyDescriptor> descriptors = Arrays.stream(Introspector.getBeanInfo(ProjectTenderInfo.class).getPropertyDescriptors())
                .collect(java.util.stream.Collectors.toMap(PropertyDescriptor::getName, descriptor -> descriptor));

        assertNotNull(descriptors.get("indexOf"));
        assertEquals(Integer.class, descriptors.get("indexOf").getPropertyType());
        assertNotNull(descriptors.get("indexOfDesc"));
        assertEquals(String.class, descriptors.get("indexOfDesc").getPropertyType());
    }
}
