package com.vesperin.base.locations;

import com.vesperin.base.Source;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Huascar Sanchez
 */
public class LocationTests {
  static final String NAME = "Name";
  static final String CONTENT = "import java.util.List; \n"
    + "class Name {\n"
    + "\tvoid boom(String msg){}\n"
    + "}";

  static final Source SOURCE = Source.from(NAME, CONTENT);

  @Test public void testLocationsBothSame() {
    final Location a = Locations.createLocation(SOURCE, CONTENT, 0, CONTENT.length());
    final Location b = Locations.createLocation(SOURCE, CONTENT, 0, CONTENT.length());

    assertThat(a.same(b), is(true));


  }

  @Test public void testLocationsOneCoversOther() {
    final Location a = Locations.createLocation(SOURCE, CONTENT, 0, CONTENT.length());
    final Location b = Locations.createLocation(SOURCE, CONTENT, 12, 20);

    assertThat(Locations.covers(a, b), is(true));

  }

  @Test public void testLocationsOneIsCoveredByOther() {
    final Location a = Locations.createLocation(SOURCE, CONTENT, 0, CONTENT.length());
    final Location b = Locations.createLocation(SOURCE, CONTENT, 12, 20);

    assertThat(Locations.covers(a, b), is(true));
  }

  @Test public void testLocationsOneIntersectsTheOther() {
    final Location a = Locations.createLocation(SOURCE, CONTENT, 0, 30);
    final Location b = Locations.createLocation(SOURCE, CONTENT, 22, 67);

    assertThat(Locations.intersects(a, b), is(true));

    final Location c = Locations.createLocation(SOURCE, CONTENT, 0, 30);
    final Location d = Locations.createLocation(SOURCE, CONTENT, 0, 5);

    assertThat(Locations.intersects(d, c), is(true));
    assertThat(c.begins(d.getStart()), is(true));
    assertThat(c.ends(d.getEnd()), is(false));
  }

  @Test public void testLocationsOneEndsInTheOther() {
    final Location a = Locations.createLocation(SOURCE, CONTENT, 0, 30);
    final Location b = Locations.createLocation(SOURCE, CONTENT, 30, 67);

    assertThat(a.ends(b.getStart()), is(true));
  }


  @Test public void testLocationsLiesOutside() {
    final Location a = Locations.createLocation(SOURCE, CONTENT, 30, 35);
    final Location b = Locations.createLocation(
      SOURCE, Locations.createPosition(0, 0, 0), Locations.createPosition(0, 0, 0));
    final Location c = Locations.createLocation(SOURCE, CONTENT, 40, 67);

    System.out.println(a);
    System.out.println(b);
    System.out.println(c);

    assertThat(Locations.outside(b, a), is(true));
    assertThat(Locations.outside(c, a), is(true));
  }


}