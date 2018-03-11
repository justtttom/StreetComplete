package de.westnordost.streetcomplete.quests.max_height;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Map;

import javax.inject.Inject;

import de.westnordost.osmapi.map.data.BoundingBox;
import de.westnordost.osmapi.map.data.Element;
import de.westnordost.streetcomplete.R;
import de.westnordost.streetcomplete.data.osm.Countries;
import de.westnordost.streetcomplete.data.osm.OsmElementQuestType;
import de.westnordost.streetcomplete.data.osm.changes.StringMapChangesBuilder;
import de.westnordost.streetcomplete.data.osm.download.MapDataWithGeometryHandler;
import de.westnordost.streetcomplete.data.osm.download.OverpassMapDataDao;
import de.westnordost.streetcomplete.data.osm.tql.FiltersParser;
import de.westnordost.streetcomplete.data.osm.tql.OverpassQLUtil;
import de.westnordost.streetcomplete.data.osm.tql.TagFilterExpression;
import de.westnordost.streetcomplete.quests.AbstractQuestAnswerFragment;

public class AddMaxHeight implements OsmElementQuestType
{

	private final OverpassMapDataDao overpassServer;

	@Inject public AddMaxHeight(OverpassMapDataDao overpassServer)
	{
		this.overpassServer = overpassServer;
	}

	private static final String QUERY_RESTRICTIONS = "!maxheight and !maxheight:physical";

	private static final String ROADS = "primary|secondary|tertiary|trunk|motorway|service|residential|unclassified|living_street|" +
		"primary_link|secondary_link|tertiary_link|trunk_link";

	private static final TagFilterExpression nodeFilter = new FiltersParser().parse("nodes with (barrier=height_restrictor" +
		" or amenity=parking_entrance and parking ~ underground|multi-storey)" +
		" and " + QUERY_RESTRICTIONS);
	private static final TagFilterExpression wayFilter = new FiltersParser().parse("ways with highway ~ " + ROADS +
		" and (covered=yes or tunnel~yes|building_passage|avalanche_protector)" +
		" and " + QUERY_RESTRICTIONS);

	@Override public boolean download(BoundingBox bbox, MapDataWithGeometryHandler handler)
	{
		return overpassServer.getAndHandleQuota(getOverpassQuery(bbox), handler);
	}

	private static String getOverpassQuery(BoundingBox bbox)
	{
		return OverpassQLUtil.getGlobalOverpassBBox(bbox) +
			nodeFilter.toOverpassQLString(null, false) +
			wayFilter.toOverpassQLString(null, false) +
			"out meta geom;";
	}

	@Override public AbstractQuestAnswerFragment createForm()
	{
		return new AddMaxHeightForm();
	}

	@Override public void applyAnswerTo(Bundle answer, StringMapChangesBuilder changes)
	{
		String maxHeight = answer.getString(AddMaxHeightForm.MAX_HEIGHT);
		int noSign = answer.getInt(AddMaxHeightForm.NO_SIGN);

		if(noSign != 0)
		{
			if(noSign == AddMaxHeightForm.IS_BELOW_DEFAULT)
				changes.add("maxheight", "below_default");
			else if(noSign == AddMaxHeightForm.IS_DEFAULT)
				changes.add("maxheight", "default");
			else if(noSign == AddMaxHeightForm.IS_NOT_INDICATED)
				changes.add("maxheight", "no_indications");
			return;
		}

		if (maxHeight != null)
		{
			changes.add("maxheight", maxHeight);
		}
	}

	@Nullable @Override public Boolean isApplicableTo(Element element)
	{
		return nodeFilter.matches(element) || wayFilter.matches(element);
	}

	@Override public String getCommitMessage() { return "Add maximum heights"; }
	@Override public int getIcon() { return R.drawable.ic_quest_max_height; }
	@Override public int getTitle(@NonNull Map<String, String> tags)
	{
		boolean isParkingEntrance = "parking_entrance".equals(tags.get("amenity"));
		boolean isHeightRestrictor = "height_restrictor".equals(tags.get("barrier"));
		boolean isTunnel = "yes".equals(tags.get("tunnel"));

		if (isParkingEntrance) return R.string.quest_maxheight_parking_entrance_title;
		if (isHeightRestrictor) return R.string.quest_maxheight_height_restrictor_title;
		if (isTunnel) return R.string.quest_maxheight_tunnel_title;

		return R.string.quest_maxheight_title;
	}

	@Override public int getTitle() { return R.string.quest_maxheight_title; }
	@Override public int getDefaultDisabledMessage() { return 0; }
	@NonNull @Override public Countries getEnabledForCountries() { return Countries.ALL; }
}
