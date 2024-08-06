package com.feed_the_beast.ftbquests.integration.jei;

import com.feed_the_beast.ftbquests.client.ClientQuestFile;
import com.feed_the_beast.ftbquests.quest.Chapter;
import com.feed_the_beast.ftbquests.quest.Quest;
import com.feed_the_beast.ftbquests.quest.reward.Reward;
import com.feed_the_beast.ftbquests.quest.reward.RewardAutoClaim;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author LatvianModder
 */
public enum QuestRegistry
{
	INSTANCE;

	public final ArrayList<QuestWrapper> list = new ArrayList<>();

	@SuppressWarnings("deprecation")
	public void refresh()
	{
		if (FTBQuestsJEIIntegration.runtime != null && !list.isEmpty())
		{
			for (QuestWrapper wrapper : list)
			{
				FTBQuestsJEIIntegration.runtime.getRecipeRegistry().removeRecipe(wrapper, QuestCategory.UID);
			}
		}

		list.clear();

		if (ClientQuestFile.exists())
		{
			List<ForkJoinTask<QuestWrapper>> tasks = new ArrayList<>();
			for (Chapter chapter : ClientQuestFile.INSTANCE.chapters)
			{
				for (Quest quest : chapter.quests)
				{
					if (quest.rewards.isEmpty() || quest.disableJEI.get(ClientQuestFile.INSTANCE.defaultQuestDisableJEI))
					{
						continue;
					}

					List<Reward> rewards = new ArrayList<>();

					for (Reward reward : quest.rewards)
					{
						if (reward.getAutoClaimType() != RewardAutoClaim.INVISIBLE && reward.getIngredient() != null)
						{
							rewards.add(reward);
						}
					}

					if (!rewards.isEmpty())
					{
						ForkJoinTask<QuestWrapper> future = ForkJoinPool.commonPool().submit(() -> new QuestWrapper(quest, rewards));
						tasks.add(future);
					}
				}
			}

            for (ForkJoinTask<QuestWrapper> task : tasks) {
                try {
                    QuestWrapper wrapper = task.get();
                    list.add(wrapper);
                    FTBQuestsJEIIntegration.runtime.getRecipeRegistry().addRecipe(wrapper, QuestCategory.UID);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
	}
}