/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.compute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AddressIdTest {

  private static final String PROJECT = "project";
  private static final String REGION = "region";
  private static final String NAME = "addr";
  private static final String GLOBAL_URL =
      "https://www.googleapis.com/compute/v1/projects/project/global/addresses/addr";
  private static final String REGION_URL =
      "https://www.googleapis.com/compute/v1/projects/project/regions/region/addresses/addr";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testOf() {
    GlobalAddressId addressId = GlobalAddressId.of(PROJECT, NAME);
    assertEquals(PROJECT, addressId.project());
    assertEquals(NAME, addressId.address());
    assertEquals(GLOBAL_URL, addressId.selfLink());
    addressId = GlobalAddressId.of(NAME);
    assertNull(addressId.project());
    assertEquals(NAME, addressId.address());
    RegionAddressId regionAddressId = RegionAddressId.of(PROJECT, REGION, NAME);
    assertEquals(PROJECT, regionAddressId.project());
    assertEquals(REGION, regionAddressId.region());
    assertEquals(NAME, regionAddressId.address());
    assertEquals(REGION_URL, regionAddressId.selfLink());
    regionAddressId = RegionAddressId.of(RegionId.of(PROJECT, REGION), NAME);
    assertEquals(PROJECT, regionAddressId.project());
    assertEquals(REGION, regionAddressId.region());
    assertEquals(NAME, regionAddressId.address());
    assertEquals(REGION_URL, regionAddressId.selfLink());
    regionAddressId = RegionAddressId.of(REGION, NAME);
    assertNull(regionAddressId.project());
    assertEquals(REGION, regionAddressId.region());
    assertEquals(NAME, regionAddressId.address());
  }

  @Test
  public void testToAndFromUrlGlobal() {
    GlobalAddressId addressId = GlobalAddressId.of(PROJECT, NAME);
    compareAddressId(addressId, GlobalAddressId.fromUrl(addressId.selfLink()));
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("notMatchingUrl is not a valid global address URL");
    GlobalAddressId.fromUrl("notMatchingUrl");
  }

  @Test
  public void testToAndFromUrlRegion() {
    RegionAddressId regionAddressId = RegionAddressId.of(PROJECT, REGION, NAME);
    compareRegionAddressId(regionAddressId, RegionAddressId.fromUrl(regionAddressId.selfLink()));
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("notMatchingUrl is not a valid region address URL");
    RegionAddressId.fromUrl("notMatchingUrl");
  }

  @Test
  public void testSetProjectId() {
    GlobalAddressId addressId = GlobalAddressId.of(PROJECT, NAME);
    assertSame(addressId, addressId.setProjectId(PROJECT));
    compareAddressId(addressId, GlobalAddressId.of(NAME).setProjectId(PROJECT));
    RegionAddressId regionAddressId = RegionAddressId.of(PROJECT, REGION, NAME);
    assertSame(regionAddressId, regionAddressId.setProjectId(PROJECT));
    compareRegionAddressId(regionAddressId, RegionAddressId.of(REGION, NAME).setProjectId(PROJECT));
  }

  @Test
  public void testMatchesUrl() {
    assertTrue(GlobalAddressId.matchesUrl(GlobalAddressId.of(PROJECT, NAME).selfLink()));
    assertFalse(GlobalAddressId.matchesUrl("notMatchingUrl"));
    assertTrue(RegionAddressId.matchesUrl(RegionAddressId.of(PROJECT, REGION, NAME).selfLink()));
    assertFalse(RegionAddressId.matchesUrl("notMatchingUrl"));
  }

  private void compareAddressId(GlobalAddressId expected, GlobalAddressId value) {
    assertEquals(expected, value);
    assertEquals(expected.project(), expected.project());
    assertEquals(expected.address(), expected.address());
    assertEquals(expected.selfLink(), expected.selfLink());
    assertEquals(expected.hashCode(), expected.hashCode());
  }

  private void compareRegionAddressId(RegionAddressId expected, RegionAddressId value) {
    assertEquals(expected, value);
    assertEquals(expected.project(), expected.project());
    assertEquals(expected.region(), expected.region());
    assertEquals(expected.address(), expected.address());
    assertEquals(expected.selfLink(), expected.selfLink());
    assertEquals(expected.hashCode(), expected.hashCode());
  }
}
