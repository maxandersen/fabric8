/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.utilities.link

import org.fusesource.fabric.apollo.amqp.codec.types.{Role, Flow}


class LinkFlowControlTracker(val role:Role) {

  var link_credit = 0L
  var delivery_count = 0L
  var available = 0L
  var drain = false

  def track(func:(Boolean) => Unit) = {
    if (link_credit <= 0) {
      available = available + 1
      func(false)
    } else {
      advance_delivery_count
      if (available > 0) {
        available = available - 1
      }
      func(true)
    }
  }

  def drain_link_credit:Unit = {
    if (drain && link_credit > 0) {
      advance_delivery_count
      drain_link_credit
    }
  }

  private def advance_delivery_count = {
    link_credit = link_credit - 1
    delivery_count = delivery_count + 1
  }
  
  def init_flow(flow:Flow = new Flow()):Flow = {
    flow.setDeliveryCount(delivery_count)
    flow.setAvailable(available)
    flow.setLinkCredit(link_credit)
    flow.setDrain(drain)
    flow
  }

  def flow(flow:Flow) = {
    role match {
      case Role.RECEIVER =>
        delivery_count = Option[Long](flow.getDeliveryCount).getOrElse(0L)
        available = Option[Long](flow.getAvailable).getOrElse(0L)
      case Role.SENDER =>
        link_credit = Option[Long](flow.getLinkCredit).getOrElse(Long.MaxValue)
        drain = Option[Boolean](flow.getDrain).getOrElse(false)
    }
  }

}