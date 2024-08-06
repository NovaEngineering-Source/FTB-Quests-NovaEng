package com.feed_the_beast.ftbquests.client;

import com.feed_the_beast.ftblib.lib.client.ClientUtils;
import com.feed_the_beast.ftblib.lib.gui.GuiBase;
import com.feed_the_beast.ftblib.net.MessageMyTeamGui;
import com.feed_the_beast.ftbquests.gui.tree.GuiQuestTree;
import com.feed_the_beast.ftbquests.integration.jei.FTBQuestsJEIHelper;
import com.feed_the_beast.ftbquests.net.MessageSyncQuests;
import com.feed_the_beast.ftbquests.net.edit.MessageDeleteObject;
import com.feed_the_beast.ftbquests.quest.Chapter;
import com.feed_the_beast.ftbquests.quest.Movable;
import com.feed_the_beast.ftbquests.quest.Quest;
import com.feed_the_beast.ftbquests.quest.QuestData;
import com.feed_the_beast.ftbquests.quest.QuestFile;
import com.feed_the_beast.ftbquests.quest.task.Task;
import com.feed_the_beast.ftbquests.quest.theme.QuestTheme;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class ClientQuestFile extends QuestFile
{
	public static ClientQuestFile INSTANCE;

	public static boolean exists()
	{
		return INSTANCE != null && !INSTANCE.invalid;
	}

	public static boolean existsWithTeam()
	{
		return exists() && INSTANCE.self != null;
	}

	private final Int2ObjectMap<ClientQuestData> teamData;
	public ClientQuestData self;
	public GuiQuestTree questTreeGui;
	public GuiBase questGui;
	public boolean editingMode;
	public final Object2IntMap<UUID> playerTeams;
	public final IntOpenHashSet pinnedQuests;

	public ClientQuestFile()
	{
		teamData = new Int2ObjectOpenHashMap<>();
		playerTeams = new Object2IntOpenHashMap<>();
		playerTeams.defaultReturnValue(0);
		pinnedQuests = new IntOpenHashSet();
	}

	public void load(MessageSyncQuests message)
	{
		if (INSTANCE != null)
		{
			INSTANCE.deleteChildren();
			INSTANCE.deleteSelf();
		}

		INSTANCE = this;

		for (MessageSyncQuests.TeamInst team : message.teamData)
		{
			ClientQuestData data = new ClientQuestData(team.uid, team.id, team.name);

			for (Chapter chapter : chapters)
			{
				for (Quest quest : chapter.quests)
				{
					for (Task task : quest.tasks)
					{
						data.createTaskData(task);
					}
				}
			}

			for (int i = 0; i < team.taskKeys.length; i++)
			{
				Task task = getTask(team.taskKeys[i]);

				if (task != null)
				{
					data.getTaskData(task).readProgress(team.taskValues[i]);
				}
			}

			for (int i = 0; i < team.playerRewardUUIDs.length; i++)
			{
				data.claimedPlayerRewards.put(team.playerRewardUUIDs[i], fromArray(team.playerRewardIDs[i]));
			}

			data.claimedTeamRewards.addAll(fromArray(team.teamRewards));
			teamData.put(data.getTeamUID(), data);
		}

		self = message.team == 0 ? null : teamData.get(message.team);
		editingMode = message.editingMode;

		playerTeams.clear();

		for (int i = 0; i < message.playerIDs.length; i++)
		{
			playerTeams.put(message.playerIDs[i], message.playerTeams[i]);
		}

		pinnedQuests.clear();

		for (int i : message.favorites)
		{
			pinnedQuests.add(i);
		}

		refreshGui();
		FTBQuestsJEIHelper.refresh(this);
	}

	private IntOpenHashSet fromArray(int[] array)
	{
		IntOpenHashSet set = new IntOpenHashSet(array.length);

		for (int i : array)
		{
			set.add(i);
		}

		return set;
	}

	@Override
	public boolean canEdit()
	{
		return editingMode;
	}

	public void refreshGui()
	{
		clearCachedData();

		boolean hasPrev = false;
		boolean guiOpen = false;
		int zoom = 0;
		double scrollX = 0, scrollY = 0;
		int selectedChapter = 0;
		int[] selectedQuests = new int[0];

		if (questTreeGui != null)
		{
			hasPrev = true;
			zoom = questTreeGui.zoom;
			scrollX = questTreeGui.questPanel.centerQuestX;
			scrollY = questTreeGui.questPanel.centerQuestY;
			selectedChapter = questTreeGui.selectedChapter == null ? 0 : questTreeGui.selectedChapter.id;
			selectedQuests = new int[questTreeGui.selectedObjects.size()];
			int i = 0;

			for (Movable m : questTreeGui.selectedObjects)
			{
				if (m instanceof Quest)
				{
					selectedQuests[i] = ((Quest) m).id;
				}

				i++;
			}

			if (ClientUtils.getCurrentGuiAs(GuiQuestTree.class) != null)
			{
				guiOpen = true;
			}
		}

		questTreeGui = new GuiQuestTree(this);
		questGui = questTreeGui;

		if (hasPrev)
		{
			questTreeGui.zoom = zoom;
			questTreeGui.selectChapter(getChapter(selectedChapter));

			for (int i : selectedQuests)
			{
				Quest q = getQuest(i);

				if (q != null)
				{
					questTreeGui.selectedObjects.add(q);
				}
			}

			if (guiOpen)
			{
				questTreeGui.openGui();
			}
		}

		questTreeGui.refreshWidgets();

		if (hasPrev)
		{
			questTreeGui.questPanel.scrollTo(scrollX, scrollY);
		}
	}

	public void openQuestGui(EntityPlayer player)
	{
		if (disableGui && !editingMode)
		{
			player.sendStatusMessage(new TextComponentTranslation("item.ftbquests.book.disabled"), true);
		}
		else if (existsWithTeam())
		{
			questGui.openGui();
		}
		else
		{
			new MessageMyTeamGui().sendToServer();
			//player.sendStatusMessage(new TextComponentTranslation("ftblib.lang.team.error.no_team"), true);
		}
	}

	@Override
	public boolean isClient()
	{
		return true;
	}

	@Nullable
	@Override
	public ClientQuestData getData(int team)
	{
		return team == 0 ? null : teamData.get(team);
	}

	@Override
	@Nullable
	public QuestData getData(UUID player)
	{
		return getData(playerTeams.getInt(player));
	}

	public ClientQuestData removeData(int team)
	{
		return teamData.remove(team);
	}

	public void addData(ClientQuestData data)
	{
		teamData.put(data.getTeamUID(), data);
	}

	@Nullable
	@Override
	public ClientQuestData getData(String team)
	{
		if (team.isEmpty())
		{
			return null;
		}

		for (ClientQuestData data : teamData.values())
		{
			if (team.equals(data.getTeamID()))
			{
				return data;
			}
		}

		return null;
	}

	@Override
	public Collection<ClientQuestData> getAllData()
	{
		return teamData.values();
	}

	@Override
	public void deleteObject(int id)
	{
		new MessageDeleteObject(id).sendToServer();
	}

	@Override
	public void clearCachedData()
	{
		super.clearCachedData();
		QuestTheme.instance.clearCache();
	}
}