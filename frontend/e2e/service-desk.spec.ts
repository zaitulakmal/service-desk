import { expect, test } from '@playwright/test'
import { signIn } from './login'

const unique = () => Math.random().toString(36).slice(2, 7)

test('staff raise a request, an agent picks it up, and the requester sees the reply', async ({ browser }) => {
  const title = `Docking station not charging ${unique()}`

  const aina = await signIn(browser, 'aina')
  await expect(aina.getByRole('heading', { name: 'My requests' })).toBeVisible()
  await aina.getByRole('link', { name: 'New request' }).first().click()
  await aina.getByLabel('What do you need help with?').fill(title)
  await aina.getByLabel('Details').fill('The dock on Level 12 stopped charging my laptop this morning.')
  await aina.getByLabel('Category').selectOption('IT')
  await aina.getByText('High', { exact: true }).click()
  await expect(aina.getByText('Target resolution: within 8 hours.')).toBeVisible()
  await aina.getByLabel('Attachments').setInputFiles({ name: 'dock.txt', mimeType: 'text/plain', buffer: Buffer.from('dock log') })
  await aina.getByRole('button', { name: 'Submit request' }).click()

  await expect(aina.getByRole('heading', { name: title })).toBeVisible()
  await expect(aina.getByText('Open', { exact: true })).toBeVisible()
  await expect(aina.getByRole('button', { name: 'dock.txt' })).toBeVisible()
  const ticketUrl = aina.url()

  const daniel = await signIn(browser, 'daniel', '/queue')
  await daniel.getByLabel('Search requests').fill(title)
  const row = daniel.getByRole('row', { name: new RegExp(title) })
  await row.getByRole('button', { name: 'Assign to me' }).click()
  await expect(row.getByText('Daniel Lee')).toBeVisible()
  await row.getByRole('link', { name: title }).click()
  await daniel.getByLabel('Write a reply').fill('Swapping the dock this afternoon.')
  await daniel.getByRole('button', { name: 'Send reply' }).click()
  // Wait for the saved message in the conversation, not the text still in the reply box.
  const danielConversation = daniel.getByRole('region', { name: 'Conversation' })
  await expect(danielConversation.getByRole('paragraph').filter({ hasText: 'Swapping the dock this afternoon.' })).toBeVisible()
  await expect(daniel.getByLabel('Write a reply')).toHaveValue('')

  await aina.goto(ticketUrl)
  await expect(aina.getByText('In progress', { exact: true }).first()).toBeVisible()
  const ainaConversation = aina.getByRole('region', { name: 'Conversation' })
  await expect(ainaConversation.getByRole('paragraph').filter({ hasText: 'Swapping the dock this afternoon.' })).toBeVisible()
})

test('an access request needs the manager and IT Security before work starts', async ({ browser }) => {
  const title = `Access to Power BI ${unique()}`

  const aina = await signIn(browser, 'aina', '/new')
  await aina.getByLabel('What do you need help with?').fill(title)
  await aina.getByLabel('Details').fill('Read access to the finance workspace.')
  await aina.getByLabel('Type of request').selectOption('ACCESS')
  await aina.getByLabel('Category').selectOption('IT')
  await expect(aina.getByText(/need approval from your manager, then IT Security/)).toBeVisible()
  await aina.getByRole('button', { name: 'Submit request' }).click()
  await expect(aina.getByText('Awaiting approval', { exact: true })).toBeVisible()
  await expect(aina.getByText('Paused for approval')).toBeVisible()

  const farah = await signIn(browser, 'farah', '/approvals')
  await farah.getByRole('link', { name: title }).click()
  await expect(farah.getByText('Your approval is needed.')).toBeVisible()
  await farah.getByRole('button', { name: 'Approve' }).click()
  await expect(farah.getByText('Your approval is needed.')).toBeHidden()

  const daniel = await signIn(browser, 'daniel', '/approvals')
  await daniel.getByRole('link', { name: title }).click()
  await daniel.getByRole('button', { name: 'Approve' }).click()
  await expect(daniel.getByText('Open', { exact: true })).toBeVisible()
  await expect(daniel.getByText('All approvals done. SLA resumed.')).toBeVisible()
})

test('menus follow roles and the service manager sees the dashboard', async ({ browser }) => {
  const aina = await signIn(browser, 'aina')
  await expect(aina.getByRole('link', { name: 'Agent queue' })).toHaveCount(0)
  await expect(aina.getByRole('link', { name: 'Dashboard' })).toHaveCount(0)

  const siti = await signIn(browser, 'siti', '/dashboard')
  await expect(siti.getByRole('heading', { name: 'Service overview' })).toBeVisible()
  await expect(siti.getByRole('button', { name: /^IT: \d+ open requests$/ })).toBeVisible()
  await siti.getByRole('button', { name: 'Table' }).click()
  await expect(siti.getByRole('table').first()).toContainText('Facilities')
})
