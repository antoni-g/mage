/*
 *  Copyright 2010 BetaSteward_at_googlemail.com. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification, are
 *  permitted provided that the following conditions are met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, this list of
 *        conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright notice, this list
 *        of conditions and the following disclaimer in the documentation and/or other materials
 *        provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY BetaSteward_at_googlemail.com ``AS IS'' AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL BetaSteward_at_googlemail.com OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *  The views and conclusions contained in the software and documentation are those of the
 *  authors and should not be interpreted as representing official policies, either expressed
 *  or implied, of BetaSteward_at_googlemail.com.
 */
package mage.cards.s;

import java.util.UUID;
import mage.MageInt;
import mage.MageObject;
import mage.abilities.Ability;
import mage.abilities.DelayedTriggeredAbility;
import mage.abilities.common.SimpleActivatedAbility;
import mage.abilities.common.delayed.AtTheBeginOfNextEndStepDelayedTriggeredAbility;
import mage.abilities.costs.common.ExileSourceCost;
import mage.abilities.costs.mana.ManaCostsImpl;
import mage.abilities.effects.ContinuousEffect;
import mage.abilities.effects.OneShotEffect;
import mage.abilities.effects.common.continuous.GainAbilityTargetEffect;
import mage.abilities.keyword.HasteAbility;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.Zone;
import mage.filter.common.FilterCreatureCard;
import mage.game.Game;
import mage.game.permanent.Permanent;
import mage.players.Player;
import mage.target.common.TargetCardInHand;
import mage.target.targetpointer.FixedTarget;

/**
 *
 * @author TheElk801
 */
public class ShiftyDoppelganger extends CardImpl {

    public ShiftyDoppelganger(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.CREATURE}, "{2}{U}");

        this.subtype.add("Shapeshifter");
        this.power = new MageInt(1);
        this.toughness = new MageInt(1);

        // {3}{U}, Exile Shifty Doppelganger: You may put a creature card from your hand onto the battlefield. If you do, that creature gains haste until end of turn. At the beginning of the next end step, sacrifice that creature. If you do, return Shifty Doppelganger to the battlefield.
        Ability ability = new SimpleActivatedAbility(Zone.BATTLEFIELD, new ShiftyDoppelgangerExileEffect(), new ManaCostsImpl("{3}{U}"));
        ability.addCost(new ExileSourceCost(true));
        this.addAbility(ability);

    }

    public ShiftyDoppelganger(final ShiftyDoppelganger card) {
        super(card);
    }

    @Override
    public ShiftyDoppelganger copy() {
        return new ShiftyDoppelganger(this);
    }
}

class ShiftyDoppelgangerExileEffect extends OneShotEffect {

    public ShiftyDoppelgangerExileEffect() {
        super(Outcome.PutCreatureInPlay);
        this.staticText = "You may put a creature card from your hand onto the battlefield. If you do, that creature gains haste until end of turn. At the beginning of the next end step, sacrifice that creature. If you do, return {this} to the battlefield";
    }

    public ShiftyDoppelgangerExileEffect(final ShiftyDoppelgangerExileEffect effect) {
        super(effect);
    }

    @Override
    public ShiftyDoppelgangerExileEffect copy() {
        return new ShiftyDoppelgangerExileEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        FilterCreatureCard filter = new FilterCreatureCard("a creature card");
        boolean putCreature = false;
        UUID creatureId = UUID.randomUUID();
        Player player = game.getPlayer(source.getControllerId());
        if (player.chooseUse(Outcome.PutCardInPlay, "Put " + filter.getMessage() + " from your hand onto the battlefield?", source, game)) {
            TargetCardInHand target = new TargetCardInHand(filter);
            if (player.choose(Outcome.PutCreatureInPlay, target, source.getSourceId(), game)) {
                Card card = game.getCard(target.getFirstTarget());
                if (card != null) {
                    putCreature = player.moveCards(card, Zone.BATTLEFIELD, source, game);
                    if (putCreature) {
                        creatureId = card.getId();
                    }
                }
            }
        }
        if (putCreature) {
            Permanent creature = game.getPermanent(creatureId);
            if (creature != null) {
                ContinuousEffect hasteEffect = new GainAbilityTargetEffect(HasteAbility.getInstance(), Duration.EndOfTurn);
                hasteEffect.setTargetPointer(new FixedTarget(creature, game));
                game.addEffect(hasteEffect, source);
                DelayedTriggeredAbility delayedAbility = new AtTheBeginOfNextEndStepDelayedTriggeredAbility(
                        new ShiftyDoppelgangerReturnEffect(creature.getId(), creature.getZoneChangeCounter(game), (int) game.getState().getValue(source.getSourceId().toString())));
                game.addDelayedTriggeredAbility(delayedAbility, source);
            }
        }
        return true;
    }
}

class ShiftyDoppelgangerReturnEffect extends OneShotEffect {

    private final UUID creatureId;
    private final int creatureZoneCount;
    private final int sourceZoneCount;

    ShiftyDoppelgangerReturnEffect(UUID creatureId, int creatureZoneCount, int sourceZoneCount) {
        super(Outcome.Benefit);
        this.staticText = "sacrifice that creature. If you do, return {this} to the battlefield";
        this.creatureId = creatureId;
        this.creatureZoneCount = creatureZoneCount;
        this.sourceZoneCount = sourceZoneCount;
    }

    ShiftyDoppelgangerReturnEffect(final ShiftyDoppelgangerReturnEffect effect) {
        super(effect);
        this.creatureId = effect.creatureId;
        this.creatureZoneCount = effect.creatureZoneCount;
        this.sourceZoneCount = effect.sourceZoneCount;
    }

    @Override
    public ShiftyDoppelgangerReturnEffect copy() {
        return new ShiftyDoppelgangerReturnEffect(this);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        Permanent creature = game.getPermanent(creatureId);
        Player player = game.getPlayer(source.getControllerId());
        MageObject sourceObject = source.getSourceObject(game);
        if (creature != null && creature.getZoneChangeCounter(game) == this.creatureZoneCount && creature.sacrifice(source.getSourceId(), game)) {
            if (player != null && sourceObject != null && sourceObject.getZoneChangeCounter(game) == this.sourceZoneCount) {
                player.moveCards(game.getCard(source.getSourceId()), Zone.BATTLEFIELD, source, game);
            }
        }
        return false;
    }
}